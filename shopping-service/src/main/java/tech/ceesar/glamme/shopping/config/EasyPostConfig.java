package tech.ceesar.glamme.shopping.config;

import com.easypost.exception.General.MissingParameterError;
import com.easypost.service.EasyPostClient;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import com.easypost.EasyPost;

@Configuration
public class EasyPostConfig {
    @Value("${easypost.apiKey}")
    private String apiKey;

    @PostConstruct
    public EasyPostClient easyPostClient() throws MissingParameterError {
        return new EasyPostClient(apiKey);
    }
}
