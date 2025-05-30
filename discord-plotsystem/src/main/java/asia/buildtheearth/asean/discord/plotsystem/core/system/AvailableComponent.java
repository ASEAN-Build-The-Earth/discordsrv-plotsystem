package asia.buildtheearth.asean.discord.plotsystem.core.system;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents all available top-level components.
 *
 * <p>Each component has a corresponding {@link SubComponent}, which is used to track the component's position and ID.</p>
 *
 * <p>The component's ID and layout position are packed into a 32-bit integer as follows:</p>
 * <table>
 *     <tr><td>Layout Position</td> <td>Component</td> <td>Subcomponent</td></tr>
 *     <tr><td>Bits 0 - 15 (max 65535)</td> <td>Bits 16 - 23 (max 255)</td> <td>Bits 24 - 31 (max 255)</td></tr>
 * </table>
 */
public enum AvailableComponent {
    UNKNOWN(null),
    // Plot information
    INFO(InfoComponent.class),
    // Plot status
    STATUS(StatusComponent.class),
    // Showcase information
    SHOWCASE(ShowcaseComponent.class);

    public interface SubComponent {
        @Contract(pure = true)
        static @Nullable SubComponent fromOrdinal(int ordinal, SubComponent[] component) {
            return (ordinal < component.length && ordinal >= 0)? component[ordinal] : null;
        }
    }

    public enum InfoComponent implements SubComponent {
        INFO_TITLE,
        INFO_LOCATION,
        INFO_GALLERY,
        INFO_SEPARATOR,
        INFO_HISTORY;
        public static final InfoComponent[] VALUES = values();
        public static @Nullable InfoComponent get(int ordinal) { return (InfoComponent) SubComponent.fromOrdinal(ordinal, VALUES); }
    }

    public enum StatusComponent implements SubComponent {
        STATUS_INFO,
        STATUS_THUMBNAIL;
        public static final StatusComponent[] VALUES = values();
        public static @Nullable StatusComponent get(int ordinal) { return (StatusComponent) SubComponent.fromOrdinal(ordinal, VALUES); }
    }

    public enum ShowcaseComponent implements SubComponent {
        SHOWCASE_INFO,
        SHOWCASE_THUMBNAIL;
        public static final ShowcaseComponent[] VALUES = values();
        public static @Nullable ShowcaseComponent get(int ordinal) { return (ShowcaseComponent) SubComponent.fromOrdinal(ordinal, VALUES); }
    }

    private final Class<? extends Enum<? extends SubComponent>> components;
    private static final AvailableComponent[] VALUES = values();

    AvailableComponent(Class<? extends Enum<? extends SubComponent>> components) {
        this.components = components;
    }

    public Class<? extends Enum<? extends SubComponent>> getComponents() {
        return components;
    }

    /**
     * Packs this top-level component ID with a dynamic position.
     * Component ID is stored in the upper 16 bits.
     *
     * @param position The dynamic position.
     * @return The packed integer.
     */
    public int pack(int position) {
        if (position < 0 || position > 0xFFFF)
            throw new IllegalArgumentException("Position must be between 0 and 65535");

        return (position & 0xFFFF) | ((this.ordinal() & 0xFF) << 16);
    }

    /**
     * Packs a subcomponent ordinal together with the parent's position.
     * Subcomponent ordinal is stored in the upper 16 bits.
     *
     * @param component The subcomponent enum constant.
     * @param position The dynamic position.
     * @return The packed integer.
     * @throws IllegalArgumentException if the given component does not belong to this AvailableComponent.
     */
    public <T extends Enum<? extends SubComponent>> int pack(@NotNull T component, int position) {
        if (this.components != component.getDeclaringClass())
            throw new IllegalArgumentException("Given component does not match parent AvailableComponent: " + this.name());

        if (position < 0 || position > 0xFFFF)
            throw new IllegalArgumentException("Position must be between 0 and 65535");

        int ordinal = component.ordinal() + 1;

        return (position & 0xFFFF) | ((this.ordinal() & 0xFF) << 16) | ((ordinal & 0xFF) << 24);
    }

    /**
     * Retrieves the top-level component by its ordinal value.
     *
     * @param ordinal The ordinal of the top-level component to retrieve.
     * @return The {@link AvailableComponent} corresponding to the given ordinal,
     *         or {@link AvailableComponent#UNKNOWN} if the ordinal is invalid.
     */
    public static AvailableComponent get(int ordinal) {
        if(ordinal < 0 || ordinal >= AvailableComponent.VALUES.length) return UNKNOWN;
        return AvailableComponent.VALUES[ordinal];
    }

    /**
     * Extracts the position from a packed ID.
     * <blockquote>{@snippet :
     *       int packedID = 1;
     *       return packedID & 0xFFFF;
     * }</blockquote>
     *
     * @param packedID The packed ID from which to extract the position.
     * @return The position stored in the lower 16 bits of the packed ID.
     */
    public static int unpackPosition(int packedID) {
        return packedID & 0xFFFF;
    }

    /**
     * Extracts the top-level component ID from a packed ID.
     * <blockquote>{@snippet :
     *       int packedID = 1;
     *       return (packedID >> 16) & 0xFF;
     * }</blockquote>
     *
     * @param packedID The packed ID from which to extract the top-level component ID.
     * @return The top-level component ID, shifted 16 bits to the right.
     */
    public static int unpackComponent(int packedID) {
        return (packedID >> 16) & 0xFF;
    }

    /**
     * Extracts the subcomponent ID from a packed ID.
     * <blockquote>{@snippet :
     *       int subID = (packedID >> 24) & 0xFF;
     *       return subID - 1;
     * }</blockquote>
     *
     * @param packedID The packed ID from which to extract the subcomponent ID.
     * @return The subcomponent ID, shifted 24 bits to the right and adjusted by subtracting 1.
     */
    public static int unpackSubComponent(int packedID) {
        int subID = (packedID >> 24) & 0xFF;
        return subID - 1;
    }
}
