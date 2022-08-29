FROM mozilla/sbt:11.0.8_1.3.13 as builder
WORKDIR /mnt
COPY build.sbt findbugs-exclude.xml ./
COPY project/ project/
COPY avldb/build.sbt avldb/build.sbt
COPY avldb/project/ avldb/project/
COPY ergo-wallet/build.sbt ergo-wallet/build.sbt
COPY ergo-wallet/project/ ergo-wallet/project/
COPY benchmarks/build.sbt benchmarks/build.sbt
RUN sbt update
COPY . ./
RUN sbt assembly
RUN mv `find target/scala-*/stripped/ -name ergo-*.jar` ergo.jar

FROM openjdk:11-jre-slim
EXPOSE 9020 9052 9030 9053
WORKDIR /root
ENV MAX_HEAP 3G
ENV _JAVA_OPTIONS "-Xmx${MAX_HEAP}"
COPY --from=builder /mnt/ergo.jar /root/ergo.jar
ENTRYPOINT ["java", "-jar", "/root/ergo.jar"]
