//package com.oau.farmerbuyer;
//
//
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//import org.testcontainers.containers.MySQLContainer;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@SpringBootTest
//@Testcontainers
//class DbSmokeIT {
//
//    @Container
//    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
//            .withDatabaseName("agrohub")
//            .withUsername("agrohub")
//            .withPassword("agrohub");
//
//    @Autowired CropRepository crops;
//
//    @Test
//    void savesCrop() {
//        var saved = crops.save(Crop.builder().name("Tomatoes").build());
//        assertThat(saved.getId()).isNotNull();
//    }
//}
