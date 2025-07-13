package tech.ceesar.glamme.booking.config;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.util.List;

@Configuration
public class GoogleCalendarConfig {
    @Value("${google.credentials.file}")
    private String credentialsPath;

    @Bean
    @SneakyThrows
    public Calendar googleCalendar() {
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new FileInputStream(credentialsPath))
                .createScoped(List.of(CalendarScopes.CALENDAR_EVENTS));
        var transport = GoogleNetHttpTransport.newTrustedTransport();
        var jsonFactory = JacksonFactory.getDefaultInstance();
        return new Calendar.Builder(transport, jsonFactory, new HttpCredentialsAdapter(credentials))
                .setApplicationName("GlammeBookingService")
                .build();
    }
}
