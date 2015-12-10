package utils.validation

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.data.FormError
import utils.validation.BooleanTupleValidator._

class BooleanTupleValidatorSpec extends PlaySpec with MockitoSugar with OneServerPerSuite {

  private val InvalidValuesForRadioButton = Seq("Invalid", "****")
  private val ValidValuesForRadioButton = Seq("First", "Second", "Third")
  private val TupleMapping = Seq((true, true), (true, false), (false, true))
  private val InvalidTupleMapping = (false,false)
  private val mappedValuesForBinding: Seq[(String, (Boolean, Boolean))] = ValidValuesForRadioButton.zip(TupleMapping)

  "BooleanTupleValidator bind" should {

    "accept valid selections" in {
      val mapping = mandatoryBooleanTuple(mappedValuesForBinding)
      mappedValuesForBinding.foreach { x =>
        mapping.bind(Map("" -> x._1)) mustBe Right(x._2)
      }
    }

    "reject invalid selection" in {
      val mapping = mandatoryBooleanTuple(mappedValuesForBinding)
      InvalidValuesForRadioButton.foreach { x =>
        mapping.bind(Map("" -> x)).left.getOrElse(Nil) must contain(FormError("", "err.pleasespecify"))
      }
    }

    "report error invalid selection" in {
      val mapping = mandatoryBooleanTuple(mappedValuesForBinding)
      InvalidValuesForRadioButton.foreach { x =>
        mapping.bind(Map()).left.getOrElse(Nil) must contain(FormError("", "err.pleasespecify"))
      }
    }

  }

  "BooleanTupleValidator unbind" should {
    "accept valid values" in {
      val mapping = mandatoryBooleanTuple(mappedValuesForBinding)
      mappedValuesForBinding.foreach { x =>
        mapping.binder.unbind("", x._2) mustBe Map("" -> x._1)
      }
    }

    "accept invalid value" in {
      val mapping = mandatoryBooleanTuple(mappedValuesForBinding)
        mapping.binder.unbind("", InvalidTupleMapping) mustBe Map("" -> "")
      }
    }
}
