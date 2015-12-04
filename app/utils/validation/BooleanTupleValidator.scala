package utils.validation

import play.api.data.format.Formatter
import play.api.data.{FieldMapping, FormError, Forms}

object BooleanTupleValidator extends FormValidator {

  val StringToBooleanTupleMappings123ToTTTFFF: Seq[(String, (Boolean, Boolean))] = Seq("1", "2", "3")
    .zip(Seq((true, true), (true, false), (false, false)))

  def mandatoryBooleanTuple(stringToBooleanTupleMappings: Seq[(String, (Boolean, Boolean))]): FieldMapping[(Boolean, Boolean)] = {

    def mandatoryBooleanTupleFormatter = new Formatter[(Boolean, Boolean)] {
      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], (Boolean, Boolean)] = {
        stringToBooleanTupleMappings find (_._1 == data(key)) match {
          case Some(x) => Right(x._2)
          case None => Left(Seq(FormError(key, "error.something")))
        }
      }

      override def unbind(key: String, value: (Boolean, Boolean)): Map[String, String] = {
        stringToBooleanTupleMappings find (_._2 == value) match {
          case Some(x) => Map(key -> x._1)
          case None => Map(key -> "")
        }
      }
    }

    Forms.of(mandatoryBooleanTupleFormatter)

  }
}
