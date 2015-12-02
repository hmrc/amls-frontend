package utils.validation

import play.api.data.validation._

trait FormValidator {
  protected lazy val ninoRegex = """^$|^[A-Z,a-z]{2}[0-9]{6}[A-D,a-d]{1}$""".r
  protected lazy val emailRegex =
    """(?i)[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9]
      |(?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?""".r
  protected lazy val postCodeRegex = ("(([gG][iI][rR] {0,}0[aA]{2})|((([a-pr-uwyzA-PR-UWYZ]" +
    "[a-hk-yA-HK-Y]?[0-9][0-9]?)|(([a-pr-uwyzA-PR-UWYZ][0-9][a-hjkstuwA-HJKSTUW])|" +
    "([a-pr-uwyzA-PR-UWYZ][a-hk-yA-HK-Y][0-9][abehmnprv-yABEHMNPRV-Y]))) {0,}[0-9]" +
    "[abd-hjlnp-uw-zABD-HJLNP-UW-Z]{2}))").r
  protected lazy val phoneNoRegex = """^[0-9 ]{10,30}$""".r//"^[A-Z0-9 \\)\\/\\(\\-\\*#]{1,27}$".r
  protected lazy val currencyRegex = """^(\d{1,11}+)$""".r
  protected lazy val sortCodeRegex = """^\d{2}(-|\s*)?\d{2}\1\d{2}$""".r
  protected lazy val accountNumberRegex = """^(\d){8}$""".r
  protected lazy val ibanRegex = """^[a-zA-Z]{2}[0-9]{2}[a-zA-Z0-9]{11,30}$""".r
  protected lazy val ukPassportNumberRegex = "^([a-zA-Z0-9]{9})|([a-zA-Z]{1}[0-9]{6})|([0-9]{6}[a-zA-Z]{1})$".r
  protected lazy val nonUkPassportNumberRegex = "^[a-zA-Z0-9]{6,40}$".r
  protected lazy val webAddressRegex = ("(https?:\\/\\/(?:www\\.|(?!www))[^\\s\\.]+\\.[^\\s]{2,}" +
    "|www\\.[^a\\s]+\\.[^\\s]{2,})").r
  val numberRegex = """^\d*$""".r

  protected def stopOnFirstFail[T](constraints: Constraint[T]*) = Constraint { field: T =>
    constraints.toList dropWhile (_(field) == Valid) match {
      case Nil => Valid
      case constraint :: _ => constraint(field)
    }
  }
}
