package utils.validation

import play.api.data.Forms
import play.api.data.format.Formatter
import play.api.data.FormError
import java.util.Currency
import scala.collection.JavaConverters._

object CurrencyValidator extends CurrencyValidator

class CurrencyValidator extends FormValidator{

  private def getCurrencies:Set[String] =
      Currency.getAvailableCurrencies.asScala.map( _.getCurrencyCode ).toSet

  private def optionalCurrencyFormatter(invalidCurrencyMessageKey: String) = new Formatter[Option[String]] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Option[String]] = {
      data.get(key) match {
        case Some(currency) => {
          currency.trim match{
            case "" => Right(None)
            case currencyTrimmed if getCurrencies.contains(currencyTrimmed) => Right(Some(currencyTrimmed))
            case _ => Left(Seq(FormError(key, invalidCurrencyMessageKey)))
          }
        }
        case _ => Left(Seq(FormError(key, invalidCurrencyMessageKey)))
      }
    }
    override def unbind(key: String, value: Option[String]): Map[String, String] = Map(key -> value.getOrElse(""))
  }

  def optionalCurrency(invalidFormatMessageKey: String) =
    Forms.of[Option[String]](optionalCurrencyFormatter(invalidFormatMessageKey))
}
