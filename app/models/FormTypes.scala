package models

object FormTypes {

  import play.api.data.mapping.forms.Rules._

  val maxNameTypeLength = 35
  val maxDescriptionTypeLength = 140
  val maxAddressLength = 35
  val maxPostCodeTypeLength = 10
  val maxCountryTypeLength = 2
  val maxPrevMLRRegNoLength = 15
  val maxVRNTypeLength = 9
  val maxPhoneNumberLength = 30
  val maxEMailLength = 100
  val maxPenalisedTypeLength = 255
  val maxAgentNameLength = 140
  val dayOrMonthLength = 2
  val yearLength = 4

  val indivNameType =
    notEmpty compose maxLength(maxNameTypeLength)

  val descriptionType =
    notEmpty compose maxLength(maxDescriptionTypeLength)

  val prevMLRRegNoType =  notEmpty compose maxLength(maxPrevMLRRegNoLength) compose pattern("^([0-9]{8}|[0-9]{15})$".r)

  val vrnType = notEmpty compose maxLength(maxVRNTypeLength) compose pattern("^[0-9]{9}$".r)

  val addressType =
    notEmpty compose maxLength(maxAddressLength)

  val postcodeType =
    notEmpty compose maxLength(maxPostCodeTypeLength)

  val countryType =
    notEmpty compose maxLength(maxCountryTypeLength)

  val phoneNumberType = notEmpty compose maxLength(maxPhoneNumberLength) compose pattern("[0-9]+".r)

  val emailType = notEmpty compose maxLength(maxEMailLength)

  val penalisedType = notEmpty compose maxLength(maxPenalisedTypeLength)

  val agentNameType = notEmpty compose maxLength(maxAgentNameLength)

  val dayOrMonthType = notEmpty compose minLength(dayOrMonthLength) compose maxLength(dayOrMonthLength) compose pattern("[0-9]+".r)

  val yearType = notEmpty compose minLength(yearLength) compose maxLength(yearLength) compose pattern("[0-9]+".r)
}

