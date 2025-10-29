//// src/test/java/com/oau/farmerbuyer/PaystackWebhookIT.java
//package com.oau.farmerbuyer;
//
//import com.oau.farmerbuyer.domain.*;
//import com.oau.farmerbuyer.repo.*;
//import io.restassured.RestAssured;
//import org.junit.jupiter.api.*;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.web.server.LocalServerPort;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.testcontainers.containers.MySQLContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//
//import javax.crypto.Mac;
//import javax.crypto.spec.SecretKeySpec;
//import java.nio.charset.StandardCharsets;
//
//import static io.restassured.RestAssured.given;
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@Testcontainers
//class PaystackWebhookIT {
//    @Container static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
//            .withDatabaseName("agrohub").withUsername("agrohub").withPassword("agrohub");
//
//    @DynamicPropertySource
//    static void dbProps(DynamicPropertyRegistry r) {
//        r.add("spring.datasource.url", mysql::getJdbcUrl);
//        r.add("spring.datasource.username", mysql::getUsername);
//        r.add("spring.datasource.password", mysql::getPassword);
//        r.add("payments.paystack.secret", () -> "psk_test_secret");
//    }
//
//    @LocalServerPort int port;
//
//    @Autowired AppUserRepository users;
//    @Autowired CropRepository crops;
//    @Autowired ListingRepository listings;
//    @Autowired OrderRepository orders;
//
//    @BeforeEach void setup() {
//        RestAssured.baseURI = "http://localhost"; RestAssured.port = port;
//        // Seed minimal order with id 1.. (or capture saved id and use it below)
//        var farmer = users.save(AppUser.builder().phoneE164("2348011111111").role(AppUser.Role.FARMER).isVerified(true).build());
//        var buyer  = users.save(AppUser.builder().phoneE164("2348022222222").role(AppUser.Role.BUYER).isVerified(true).build());
//        var crop   = crops.save(Crop.builder().name("Tomatoes").build());
//        var listing = listings.save(Listing.builder().farmer(farmer).crop(crop).title("t").unit(Crop.Unit.KG)
//                .quantityAvailable(new java.math.BigDecimal("100")).pricePerUnitNgn(new java.math.BigDecimal("500")).build());
//        var order = orders.save(Order.builder().buyer(buyer).farmer(farmer).listing(listing)
//                .quantityOrdered(new java.math.BigDecimal("1")).unitPriceSnapshot(new java.math.BigDecimal("500"))
//                .subtotalNgn(new java.math.BigDecimal("500")).platformFeeNgn(new java.math.BigDecimal("100"))
//                .totalNgn(new java.math.BigDecimal("600")).build());
//    }
//
//    private String hmac512(String secret, String body) throws Exception {
//        var mac = Mac.getInstance("HmacSHA512");
//        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
//        var bytes = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
//        var sb = new StringBuilder(); for (byte b: bytes) sb.append(String.format("%02x", b)); return sb.toString();
//    }
//
//    @Test
//    void rejects_invalid_signature() {
//        var body = "{\"event\":\"charge.success\",\"data\":{\"reference\":\"order-1\",\"amount\":500000}}";
//        given().header("x-paystack-signature", "bad")
//                .contentType("application/json").body(body)
//                .when().post("/api/payments/paystack/webhook")
//                .then().statusCode(401);
//    }
//
//    @Test
//    void accepts_valid_signature() throws Exception {
//        var body = "{\"event\":\"charge.success\",\"data\":{\"reference\":\"order-1\",\"amount\":500000}}";
//        var sig = hmac512("psk_test_secret", body);
//        given().header("x-paystack-signature", sig)
//                .contentType("application/json").body(body)
//                .when().post("/api/payments/paystack/webhook")
//                .then().statusCode(200);
//    }
//}
