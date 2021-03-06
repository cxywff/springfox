package springfox.documentation.builders;

import java.util.List;

public interface Validator<T> {
  List<ValidationResult> validate(T builder);
}
