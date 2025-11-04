import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UserTest {

    @Test
    void testUserCreation() {
        User user = new User("Test User");
        
        assertNotNull(user.getId());
        assertEquals("Test User", user.getName());
        assertTrue(user.getId().length() > 0);
    }

    @Test
    void testUserEquality() {
        User user1 = new User("user1", "User One");
        User user2 = new User("user1", "User One");
        User user3 = new User("user2", "User Two");
        
        assertEquals(user1, user2);
        assertNotEquals(user1, user3);
        assertEquals(user1.hashCode(), user2.hashCode());
    }

    @Test
    void testUserToString() {
        User user = new User("test-id", "Test User");
        String toString = user.toString();
        
        assertTrue(toString.contains("Test User"));
        assertTrue(toString.contains("test-id"));
    }
}