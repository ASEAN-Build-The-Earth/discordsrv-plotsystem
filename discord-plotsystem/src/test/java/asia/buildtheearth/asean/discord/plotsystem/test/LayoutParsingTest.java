package asia.buildtheearth.asean.discord.plotsystem.test;

import asia.buildtheearth.asean.discord.components.api.Container;
import asia.buildtheearth.asean.discord.plotsystem.core.system.AvailableComponent;
import asia.buildtheearth.asean.discord.plotsystem.core.system.layout.Layout;
import asia.buildtheearth.asean.discord.plotsystem.core.system.layout.NotificationComponent;
import asia.buildtheearth.asean.discord.plotsystem.test.mock.MockSnowflake;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataArray;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;

@DisplayName("Display Components")
public class LayoutParsingTest {

    @Test
    @DisplayName("Test building & parsing notification component")
    public void testNotificationComponent() {

        NotificationComponent component = new NotificationComponent(
            0, AvailableComponent.NotificationComponent.ON_INACTIVITY,
            "## :warning: <@{snowflake}> Your plot is getting abandoned <t:1777939200:R>"
            .replace("{snowflake}", Long.toUnsignedString(MockSnowflake.getRandom()))
            + "\n-# Plot is detected inactive, please join your plot world to extend this."
        );

        Container expected = component.build();
        DataArray testArray = DataArray.fromCollection(Collections.singleton(expected));

        Optional<Layout> parseWrongly = Layout.fromRawData(testArray, AvailableComponent.INFO);
        Assertions.assertTrue(parseWrongly.isEmpty(), "Layout#fromRawData should whitelist correct parsing type.");
        
        Optional<Layout> parse = Layout.fromRawData(testArray, AvailableComponent.NOTIFICATION);
        Assertions.assertTrue(parse.isPresent(), "Layout#fromRawData should parse notification component.");

        Layout.LayoutData layout = parse.get().getLayout();
        Assertions.assertEquals(1, layout.size());

        NotificationComponent result = Assertions.assertInstanceOf(NotificationComponent.class, layout.getFirst());
        Assertions.assertEquals(AvailableComponent.NotificationComponent.ON_INACTIVITY, result.getNotificationType());
        Assertions.assertEquals(expected.toString(), result.build().toString());
    }
}
