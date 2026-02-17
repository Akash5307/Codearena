dependencies {
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-web")

    // MinIO
    implementation("io.minio:minio:8.5.12")

    // Docker Java API
    implementation("com.github.docker-java:docker-java-core:3.4.0")
    implementation("com.github.docker-java:docker-java-transport-httpclient5:3.4.0")
}
