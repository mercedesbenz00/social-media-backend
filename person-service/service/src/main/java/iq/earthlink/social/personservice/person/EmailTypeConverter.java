package iq.earthlink.social.personservice.person;

import iq.earthlink.social.classes.enumeration.EmailType;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class EmailTypeConverter implements AttributeConverter<EmailType, String> {

    @Override
    public String convertToDatabaseColumn(EmailType emailType) {
        if (emailType == null) {
            return null;
        }
        return emailType.getCode();
    }

    @Override
    public EmailType convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }

        return EmailType.fromCode(code);
    }
}
