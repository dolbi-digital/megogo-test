package api;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utils.ConfigLoader;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.testng.Assert.assertTrue;

public class MegogoTest {

    @DataProvider(name = "videoIds", parallel = true)
    public Object[][] videoIdsProvider() {
        return new Object[][]{
                {1639111},
                {1585681},
                {1639231}
        };
    }

    @BeforeClass
    public void setup() {
        ConfigLoader.loadConfig();
        RestAssured.baseURI = ConfigLoader.getBaseURI();
    }

    @Test
    public void currentTimeTest() {
        Instant currentTimeBeforeRequest = Instant.now();

        Response response = given()
                .filter(new AllureRestAssured())
                .when()
                .get("/time")
                .then()
                .statusCode(200)
                .body("result", equalTo("ok"))
                .extract()
                .response();

        long serverTimestamp = response.jsonPath().getLong("data.timestamp_gmt");
        Instant currentTimeAfterRequest = Instant.now();
        Instant serverTime = Instant.ofEpochSecond(serverTimestamp);

        System.out.println("Local time before request: " + currentTimeBeforeRequest);
        System.out.println("Server time:               " + serverTime);
        System.out.println("Local time after request:  " + currentTimeAfterRequest);

        assertTrue(serverTime.isAfter(currentTimeBeforeRequest.minus(5, ChronoUnit.SECONDS)));
        assertTrue(serverTime.isBefore(currentTimeAfterRequest.plus(5, ChronoUnit.SECONDS)));
    }

    @Test(dataProvider = "videoIds")
    public void scheduleTVTest(int videoId) {

        Response response = given()
                .filter(new AllureRestAssured())
                .queryParam("video_ids", videoId)
                .when()
                .get("/channel")
                .then()
                .statusCode(200)
                .body("result", equalTo("ok"))
                .extract()
                .response();

        List<Map<String, Object>> programs = response.jsonPath().getList("data[0].programs");

        long currentTimestamp = Instant.now().getEpochSecond();

        // a) Are programs sorted by "start_timestamp"?
        boolean isSorted = true;
        for (int i = 1; i < programs.size(); i++) {
            int previousStart = (int) programs.get(i - 1).get("start_timestamp");
            int currentStart = (int) programs.get(i).get("start_timestamp");
            if (currentStart < previousStart) {
                isSorted = false;
                break;
            }
        }
        assertTrue(isSorted, "Programs not sorted by start_timestamp!");

        // b): Is program in the TV schedule on the current time?
        boolean hasCurrentProgram = false;
        for (Map<String, Object> program : programs) {
            long startTimestamp = ((Number) program.get("start_timestamp")).longValue();
            long endTimestamp = ((Number) program.get("end_timestamp")).longValue();
            if (currentTimestamp >= startTimestamp && currentTimestamp <= endTimestamp) {
                hasCurrentProgram = true;
                break;
            }
        }
        assertTrue(hasCurrentProgram, "No program in the TV schedule on the current time!");

        // c) Is there a program from the past or more than 24 hours ahead?
        long twentyFourHoursInSeconds = 24 * 60 * 60;
        for (Map<String, Object> program : programs) {
            long startTimestamp = ((Number) program.get("start_timestamp")).longValue();
            long endTimestamp = ((Number) program.get("end_timestamp")).longValue();
            assertTrue(endTimestamp >= currentTimestamp, "There are programs from the past!");
            assertTrue(startTimestamp <= currentTimestamp + twentyFourHoursInSeconds,
                    "There are programs more than 24 hours ahead!");
        }
    }
}
