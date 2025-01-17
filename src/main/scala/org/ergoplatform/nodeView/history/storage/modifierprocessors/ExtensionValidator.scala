package org.ergoplatform.nodeView.history.storage.modifierprocessors

import org.ergoplatform.modifiers.history.extension.{Extension, ExtensionCandidate}
import org.ergoplatform.modifiers.history.header.Header
import org.ergoplatform.modifiers.history.popow.NipopowAlgos
import org.ergoplatform.settings.ValidationRules._
import scorex.core.utils.ScorexEncoding
import scorex.core.validation.{InvalidModifier, ValidationState}
import scorex.util.bytesToId

/**
  * Class that implements extension validation based on current to ErgoValidationSettings
  */
class ExtensionValidator[T](validationState: ValidationState[T], popowAlgos: NipopowAlgos) extends ScorexEncoding {

  def validateExtension(extension: Extension,
                        header: Header,
                        prevExtensionOpt: Option[ExtensionCandidate],
                        prevHeaderOpt: Option[Header]): ValidationState[T] = {
    validateInterlinks(extension, header, prevExtensionOpt, prevHeaderOpt)
      .validate(exKeyLength, extension.fields.forall(_._1.lengthCompare(Extension.FieldKeySize) == 0), InvalidModifier(extension.encodedId, extension.id, extension.modifierTypeId))
      .validate(exValueLength, extension.fields.forall(_._2.lengthCompare(Extension.FieldValueMaxSize) <= 0), InvalidModifier(extension.encodedId, extension.id, extension.modifierTypeId))
      .validate(exDuplicateKeys, extension.fields.map(kv => bytesToId(kv._1)).distinct.length == extension.fields.length, InvalidModifier(extension.encodedId, extension.id, extension.modifierTypeId))
      .validate(exEmpty, header.isGenesis || extension.fields.nonEmpty, InvalidModifier(extension.encodedId, extension.id, extension.modifierTypeId))
  }

  private def validateInterlinks(extension: Extension,
                                 header: Header,
                                 prevExtensionOpt: Option[ExtensionCandidate],
                                 prevHeaderOpt: Option[Header]): ValidationState[T] = {
    (prevHeaderOpt, prevExtensionOpt) match {
      case (Some(parent), Some(parentExt)) =>
        val parentLinksTry = NipopowAlgos.unpackInterlinks(parentExt.fields)
        val currentLinksTry = NipopowAlgos.unpackInterlinks(extension.fields)

        val expectedLinksTry = parentLinksTry
          .map { prev => popowAlgos.updateInterlinks(parent, prev) }

        validationState
          .validateNoFailure(exIlEncoding, currentLinksTry, extension.id, extension.modifierTypeId)
          .validate(exIlStructure, expectedLinksTry == currentLinksTry, InvalidModifier(s"$expectedLinksTry == $currentLinksTry", extension.id, extension.modifierTypeId))
      case _ =>
        validationState
          .validate(exIlUnableToValidate, header.isGenesis || prevExtensionOpt.isEmpty, InvalidModifier(header.id, extension.id, extension.modifierTypeId))
    }
  }
}
