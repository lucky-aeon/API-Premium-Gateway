import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "logging.level.org.xhy.gateway.domain.apiinstance.strategy.SmartStrategy=DEBUG",
    "logging.level.org.xhy.gateway=DEBUG"
})
public class TestSmartStrategy {
    
    @Test
    public void testBasic() {
        System.out.println("Smart strategy debug test");
    }
} 