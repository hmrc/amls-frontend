package models

object FormTypes {

  import play.api.data.mapping.forms.Rules._

  val maxNameTypeLength = 35
  val maxDescriptionTypeLength = 255
  val maxAddressLength = 35
  val maxPostCodeLength = 10
  val maxCountryTypeLength = 2
  val maxPrevMLRRegNoLength = 15

  def indivNameType =
    notEmpty |+| maxLength(maxNameTypeLength)

  def descriptionType =
    notEmpty |+| maxLength(maxDescriptionTypeLength)

  val prevMLRRegNoType =  notEmpty |+| maxLength(maxPrevMLRRegNoLength) compose pattern("^([0-9]{8}|[0-9]{15})$".r)


  def validAddressType =
    notEmpty |+| maxLength(maxAddressLength)

  def validPostCodeType =
    notEmpty |+| maxLength((maxPostCodeLength))

  def validCountryType =
    notEmpty |+| maxLength(maxCountryTypeLength)

}
