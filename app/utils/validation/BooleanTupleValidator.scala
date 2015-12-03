package utils.validation

import play.api.data.format.Formatter
import play.api.data.{FieldMapping, FormError, Forms}

object BooleanTupleValidator extends FormValidator {

  def mandatoryBooleanTuple(tuples: Seq[(String, (Boolean, Boolean))]): FieldMapping[(Boolean, Boolean)] = {

    val validationTuples = tuples

    def mandatoryBooleanTupleFormatter = new Formatter[(Boolean, Boolean)] {
      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], (Boolean, Boolean)] = {
        validationTuples find (_._1 == data(key)) match {
          case Some(x) => Right(x._2)
          case None => Left(Seq(FormError(key, "error.something")))
        }
      }

      override def unbind(key: String, value: (Boolean, Boolean)): Map[String, String] = {
        validationTuples find (_._2 == value) match {
          case Some(x) => Map(key -> x._1)
          case None => Map(key -> "")
        }
      }
    }

    Forms.of(mandatoryBooleanTupleFormatter)

  }
}
