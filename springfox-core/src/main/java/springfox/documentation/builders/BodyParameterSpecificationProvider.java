package springfox.documentation.builders;

import org.slf4j.Logger;
import org.springframework.http.MediaType;
import springfox.documentation.schema.ScalarType;
import springfox.documentation.service.ContentSpecification;
import springfox.documentation.service.ParameterSpecification;
import springfox.documentation.service.Representation;
import springfox.documentation.service.SimpleParameterSpecification;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.*;

public class BodyParameterSpecificationProvider implements ParameterSpecificationProvider {
  private static final Logger LOGGER = getLogger(BodyParameterSpecificationProvider.class);


  @Override
  public ParameterSpecification create(ParameterSpecificationContext context) {
    SimpleParameterSpecification simpleParameter = context.getSimpleParameter();
    ContentSpecification contentParameter = context.getContentParameter();

    Collection<MediaType> accepts = new HashSet<>(context.getAccepts());
    if (contentParameter != null) {
      accepts.addAll(contentParameter.getRepresentations().stream()
                                     .map(Representation::getMediaType)
                                     .collect(Collectors.toSet()));
    }
    if (accepts.isEmpty()) {
      accepts.add(MediaType.ALL);
    }

    accepts.stream()
           .filter(mediaType -> !mediaType.equalsTypeAndSubtype(MediaType.APPLICATION_FORM_URLENCODED))
           .forEach(
               each -> {
                 if (simpleParameter != null && simpleParameter.getModel() != null) {
                   context.getContentSpecificationBuilder()
                          .copyOf(contentParameter)
                          .requestBody(true)
                          .representation(each)
                          .apply(r -> r
                              .model(m -> m.copyOf(simpleParameter.getModel()))
                              .encodings(null));
                 } else if (contentParameter != null) {
                   Optional<Representation> mediaType = contentParameter.representationFor(each);
                   context.getContentSpecificationBuilder()
                          .copyOf(contentParameter)
                          .requestBody(true)
                          .representation(each)
                          .apply(r -> r.model(m -> m.copyOf(mediaType
                                                                .map(Representation::getModel)
                                                                .orElse(new ModelSpecificationBuilder()
                                                                            .name(context.getName())
                                                                            .scalarModel(ScalarType.STRING)
                                                                            .build())))
                                       .encodings(null));

                 } else {
                   LOGGER.warn("Parameter should either be a simple or a content type");
                   context.getContentSpecificationBuilder()
                          .requestBody(true)
                          .representation(each)
                          .apply(r -> r.model(m -> m
                              .copyOf(new ModelSpecificationBuilder()
                                          .name(context.getName())
                                          .scalarModel(ScalarType.STRING)
                                          .build()))
                                       .encodings(null));
                 }
               });
    return new ParameterSpecification(null, context.getContentSpecificationBuilder().build());
  }
}
