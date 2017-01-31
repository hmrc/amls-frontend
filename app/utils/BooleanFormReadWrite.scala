package utils

import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, Write}
import utils.MappingUtils.Implicits._
import jto.validation.forms.Rules._

object BooleanFormReadWrite {
   def formWrites(fieldName:String) : Write[Boolean, UrlFormEncoded] = Write{data : Boolean => Map(fieldName -> Seq(data.toString))}

   def formRule(fieldName:String) : Rule[UrlFormEncoded, Boolean] = From[UrlFormEncoded] { __ =>
      (__ \ fieldName).read[Boolean].withMessage("error.required.rp.fit_and_proper")
   }
}
