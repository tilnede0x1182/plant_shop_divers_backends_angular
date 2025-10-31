import io.micronaut.inject.visitor.TypeElementVisitor;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

public final class ProcessorCheck {

    private ProcessorCheck() {
    }

    public static void main(String[] args) {
        ServiceLoader<TypeElementVisitor> loader = ServiceLoader.load(TypeElementVisitor.class);
        try {
            for (TypeElementVisitor<?, ?> visitor : loader) {
                System.out.println("Loaded: " + visitor.getClass().getName());
            }
        } catch (ServiceConfigurationError e) {
            e.printStackTrace(System.err);
        }
    }
}
