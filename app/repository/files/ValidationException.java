package repository.files;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class ValidationException extends Exception {

    private static final long serialVersionUID = -7576342454500384799L;
    public Map<String, String> errors = new HashMap<String, String>();

    public static void validate(Node node) throws ValidationException {
        ValidationException e = new ValidationException();
        node.validate(e);
        if (!e.errors.isEmpty()) {
            throw e;
        }
    }

    public void required(String s, String name) {
        if (StringUtils.isEmpty(s)) {
            errors.put(name, name + " is required");
        }
    }

    public void required(Object s, String name) {
        if (s == null) {
            errors.put(name, name + " is required");
        }
    }

    public void maybeThrow() throws ValidationException {
        if (!errors.isEmpty()) {
            throw this;
        }
    }

    @Override
    public String getMessage() {
        return StringUtils.join(errors);
    }
    
    
}