package models

object FormTypes {

  import play.api.data.mapping.forms.Rules._

  val maxNameTypeLength = 35
  val maxDescriptionTypeLength = 255
  val maxAddressLength = 35
  val maxPostCodeTypeLength = 10
  val maxCountryTypeLength = 2
  val maxPrevMLRRegNoLength = 15

  def indivNameType =
    notEmpty compose maxLength(maxNameTypeLength)

  def descriptionType =
    notEmpty compose maxLength(maxDescriptionTypeLength)

  val prevMLRRegNoType =  notEmpty compose maxLength(maxPrevMLRRegNoLength) compose pattern("^([0-9]{8}|[0-9]{15})$".r)


  def addressType =
    notEmpty compose maxLength(maxAddressLength)

  def postCodeType =
    notEmpty compose maxLength(maxPostCodeTypeLength)

  def countryType =
    notEmpty compose maxLength(maxCountryTypeLength)

}
