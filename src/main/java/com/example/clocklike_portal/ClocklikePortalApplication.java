package com.example.clocklike_portal;

import com.example.clocklike_portal.app.CustomPropertySource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class ClocklikePortalApplication {
    // TODO przydałyby się profile
    //TODO  uprościć do C:/settings.txt ??
    private final static String DEV_DIRECTORY = "C:/Users/piotr/OneDrive/Pulpit/settings.txt";
    private final static String PROD_DIRECTORY = "";


    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(ClocklikePortalApplication.class);
        application.addInitializers(new ApplicationContextInitializer<GenericApplicationContext>() {
            @Override
            public void initialize(GenericApplicationContext applicationContext) {
                Environment env = applicationContext.getEnvironment();
                String activeProfile = env.getActiveProfiles().length > 0 ? env.getActiveProfiles()[0] : "default";
                String propertiesFile;

                switch (activeProfile) {
                    case "prod":
                        propertiesFile = PROD_DIRECTORY;
                        break;
                    case "dev":
                    default:
                        propertiesFile = DEV_DIRECTORY;
                        break;
                }
                applicationContext.getEnvironment().getPropertySources()
                        .addFirst(new CustomPropertySource("customProperties", propertiesFile));
            }
        });
        application.run(args);
    }

}
