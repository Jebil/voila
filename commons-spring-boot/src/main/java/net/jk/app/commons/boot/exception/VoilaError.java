package net.jk.app.commons.boot.exception;

import lombok.Getter;

/** Enum of application errors and their message keys for internationalization. */
@Getter
public enum VoilaError implements IVoilaError {
  FIELD_MUST_NOT_BE_EMPTY("fieldMustNotBeEmpty"),
  VALUES_MUST_BE_DISTINCT("valuesMustBeDistinct"),
  ENTITY_ALREADY_EXISTS("entityAlreadyExists"),
  ENTITY_DOES_NOT_EXIST("entityDoesNotExist"),
  ENTITY_MULTIPLE_EXIST("entityMultipleExists"),
  INVALID_QUERY_SYNTAX("invalidQuerySyntax"),
  NON_UPDATEABLE_FIELD_MOD("nonUpdateableFieldModification"),
  PUBLIC_ID_CONFLICT("publicIdConflict"),
  QUERY_PARAM_REQUIRED("queryParamRequired"),
  // AUTHENTICATION ERRORS
  USER_DEACTIVATED("securityUserDeactivated"),
  INVALID_USER_OR_CREDS("invalidUserOrCredentials"),
  USER_NOT_AUTHORIZED("userNotAuthorized"),
  NON_UPDATEABLE_ENTITY("nonUpdateableEntity"),
  ENTITY_CANNOT_BE_DELETED("enityCannotBeDeleted"),
  INVALID_RANGE_UPPER_BOUND("invalidRangeUpperBound"),
  OVERLAPPING_RANGE("overlappingRange"),
  INVALID_VALUE_FOR_ATTRIBUTE("invalidValueForAttribute"),
  MIN_NUMERIC_VALUE_REQUIRED("minNumericValueRequired"),
  INVALID_SYNTAX("invalidSyntax"),
  FIELD_NOT_FOUND("fieldNotFound"),
  NOT_APPLICABLE_DATACONTEXT("notApplicableDataContext"),
  NOT_APPLICABLE_ROLE("notApplicableRole"),
  ALL_FIELDS_REQUIRED("allFieldsRequired"),
  NOT_IMPLEMENTED("notImplemented");

  private final String messageKey;

  VoilaError(String messageKey) {
    this.messageKey = messageKey;
  }
}
