package models

object FormTypes {

  import play.api.data.mapping.forms.Rules._

  def indivNameType =
    notEmpty |+| maxLength(35)

  def descriptionType =
    notEmpty |+| maxLength(255)
}
