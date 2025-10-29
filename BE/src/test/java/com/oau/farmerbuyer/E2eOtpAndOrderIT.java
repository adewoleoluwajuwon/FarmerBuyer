//package com.oau.farmerbuyer;
//
//
//
//import com.oau.farmerbuyer.domain.AppUser;
//import com.oau.farmerbuyer.domain.Crop;
//import com.oau.farmerbuyer.repository.AppUserRepository;
//import com.oau.farmerbuyer.repository.CropRepository;
//import com.oau.farmerbuyer.repository.OrderRepository;
//import io.restassured.RestAssured;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.web.server.LocalServerPort;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.testcontainers.containers.MySQLContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//
//import static io.restassured.RestAssured.given;
//import static org.hamcrest.Matchers.notNullValue;
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@Testcontainers
//class E2eOtpAndOrderIT {
//
//    @Container
//    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
//            .withDatabaseName("agrohub")
//            .withUsername("agrohub")
//            .withPassword("agrohub");
//
//    @DynamicPropertySource
//    static void dbProps(DynamicPropertyRegistry r) {
//        r.add("spring.datasource.url", mysql::getJdbcUrl);
//        r.add("spring.datasource.username", mysql::getUsername);
//        r.add("spring.datasource.password", mysql::getPassword);
//    }
//
//    @LocalServerPort
//    int port;
//
//    @Autowired AppUserRepository users;
//    @Autowired CropRepository crops;
//    @Autowired OrderRepository orders;
//
//    @BeforeEach
//    void setup() {
//        RestAssured.baseURI = "http://localhost";
//        RestAssured.port = port;
//    }
//
//    @Test
//    void otp_roundtrip_and_place_order() {
//        var farmer = users.save(AppUser.builder()
//                .phoneE164("2348011111111").fullName("Farmer")
//                .role(AppUser.Role.FARMER).isVerified(true).build());
//        var buyer  = users.save(AppUser.builder()
//                .phoneE164("2348022222222").fullName("Buyer")
//                .role(AppUser.Role.BUYER).isVerified(true).build());
//        var crop = crops.save(Crop.builder().name("Tomatoes").build());
//
//        // OTP request (dev mode echoes code via app.otp.echoInResponse=true)
//        var devCode = given().contentType("application/json")
//                .body("{\"phone\":\"2348099999999\"}")
//                .when().post("/api/auth/otp/request")
//                .then().statusCode(200)
//                .extract().path("devCode");
//
//        // OTP verify
//        var token = given().contentType("application/json")
//                .body("{\"phone\":\"2348099999999\",\"code\":\"" + devCode + "\"}")
//                .when().post("/api/auth/otp/verify")
//                .then().statusCode(200).body("token", notNullValue())
//                .extract().path("token");
//
//        Assertions.assertNotNull(token);
//    }
//}
