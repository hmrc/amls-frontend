package utils.validation

import play.api.data.Forms
import play.api.data.format.Formatter
import play.api.data.FormError

object CurrencyValidator extends CurrencyValidator

class CurrencyValidator extends FormValidator{

  private def cleanMoneyString(moneyString: String) =
    currencyRegex.findFirstIn(moneyString.replace(",","")).getOrElse("")

  private def optionalCurrencyFormatter(invalidFormatMessageKey: String) = new Formatter[Option[BigDecimal]] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Option[BigDecimal]] = {
      data.get(key) match {
        case Some(num) => {
          num.trim match{
            case "" => Right(None)
            case numTrimmed => {
              try {
                val bigDecimalMoney = BigDecimal(cleanMoneyString(numTrimmed))
                Right(Some(bigDecimalMoney))
              } catch {
                case e: NumberFormatException => Left(Seq(FormError(key, invalidFormatMessageKey)))
              }
            }
          }
        }
        case _ => Left(Seq(FormError(key, invalidFormatMessageKey)))
      }
    }
    override def unbind(key: String, value: Option[BigDecimal]): Map[String, String] = Map(key -> value.getOrElse("").toString)
  }

  def optionalCurrency(invalidFormatMessageKey: String) =
    Forms.of[Option[BigDecimal]](optionalCurrencyFormatter(invalidFormatMessageKey))
}
