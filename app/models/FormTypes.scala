package models

object FormTypes {

  import play.api.data.mapping.forms.Rules._

  val maxLengthName = 35
  val maxDescriptionType = 255
  val maxAddressLength = 35
  val maxPostCodeLength = 10
  val maxCountryTypeLength = 2

  def indivNameType =
    notEmpty |+| maxLength(maxLengthName)

  def descriptionType =
    notEmpty |+| maxLength(maxDescriptionType)

  def validateAddressType =
    notEmpty |+| maxLength(maxAddressLength)

  def validPostCode =
    notEmpty |+| maxLength((maxPostCodeLength))

  def validCountryType =
    notEmpty |+| maxLength(maxCountryTypeLength)

}
