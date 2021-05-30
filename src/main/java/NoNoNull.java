import org.jetbrains.annotations.NotNull;
//import javax.annotation.Nonnull;

public class NoNoNull {

    @NotNull
    //@Nonnull
    public static String definitelyNotNull() {
        return "asdf";
    }

    public static String maybeNotNull() {
        return "asdf";
    }

}
