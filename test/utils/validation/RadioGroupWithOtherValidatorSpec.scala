package utils.validation

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.data.FormError
import utils.validation.RadioGroupWithOtherValidator._

class RadioGroupWithOtherValidatorSpec extends PlaySpec with MockitoSugar  with OneServerPerSuite{
    val MAX_LENGTH = 100
    "mandatoryBoolean" should {

     "respond appropriately if Yes is chosen and a value is entered into the text field" in {
       val mapping = radioGroupWithOther("otherValue","Yes", "No radio button selected", "blank value", "Invalid length", MAX_LENGTH)
       mapping.bind(Map("" -> "Yes", "otherValue" -> "random")) mustBe Right("Yes")
     }

     "respond appropriately if Yes is chosen and no value entered into the text field" in {
       val mapping = radioGroupWithOther("otherValue","Yes", "No radio button selected", "blank value", "Invalid length", MAX_LENGTH)
       mapping.bind(Map("otherValue" -> "", "" -> "Yes"))
         .left.getOrElse(Nil).contains(FormError("otherValue", "blank value")) mustBe true
     }

     "respond appropriately if Yes is chosen and value entered into the text field is too long" in {
       val mapping = radioGroupWithOther("otherValue","Yes", "No radio button selected", "blank value", "Invalid length", MAX_LENGTH)
       mapping.bind(Map("otherValue" -> "aaaaaaaaaa" * 11, "" -> "Yes"))
         .left.getOrElse(Nil).contains(FormError("otherValue", "Invalid length")) mustBe true
     }

     "respond appropriately if No is chosen and no value entered into the text field" in {
       val mapping = radioGroupWithOther("otherValue","Yes", "No radio button selected", "blank value", "Invalid length", MAX_LENGTH)
       mapping.bind(Map("otherValue" -> "", "" -> "No")) mustBe Right("No")
     }
   }
 }
