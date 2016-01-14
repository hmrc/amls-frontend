package models

object FormTypes {

  import play.api.data.mapping.forms.Rules._

  def indivNameType =
    notEmpty |+| maxLength(35)

  def descriptionType =
    notEmpty |+| maxLength(255)

  val prevMLRRegNoType =  notEmpty |+| maxLength(15) compose pattern("^([0-9]{8}|[0-9]{15})$".r)

  val VATRegNoType =  notEmpty |+| maxLength(9) compose pattern("^[0-9]{9}$".r)
}
