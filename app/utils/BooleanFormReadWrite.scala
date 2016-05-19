package utils

import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping.{From, Rule, Write}
import utils.MappingUtils.Implicits._
import play.api.data.mapping.forms.Rules._

object BooleanFormReadWrite {
   def formWrites(fieldName:String) : Write[Boolean, UrlFormEncoded] = Write{data : Boolean => Map(fieldName -> Seq(data.toString))}

   def formRule(fieldName:String) : Rule[UrlFormEncoded, Boolean] = From[UrlFormEncoded] { __ =>
      (__ \ fieldName).read[Boolean].withMessage("error.required.rp.fit_and_proper")
   }
}
