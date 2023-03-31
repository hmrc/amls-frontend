/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package forms.behaviours

import org.scalacheck.Gen
import play.api.data.{Form, FormError}
import collection.mutable.{Map => MutableMap}


trait AddressFieldBehaviours extends FieldBehaviours {

  def fieldWithMaxLength(form: Form[_],
                         extraData: MutableMap[String, String],
                         fieldName: String,
                         maxLength: Int,
                         lengthError: FormError): Unit = {

    s"not bind strings longer than $maxLength characters" in {

      forAll(Gen.alphaNumStr.suchThat(_.length > maxLength)) { string =>

        val formData: MutableMap[String, String] = extraData += (fieldName -> string)
        val newForm = form.bind(Map(formData.toSeq.sortBy(_._1): _*))

        val result = newForm.apply(fieldName)

        result.errors shouldEqual Seq(lengthError)
      }
    }
  }
  def fieldWithRegexValidation(form: Form[_],
                               extraData: MutableMap[String, String],
                               fieldName: String,
                               regex: String,
                               regexError: FormError): Unit = {

    s"not bind strings that violate regex" in {

      val invalidStrs = List(
        "WXN\"XYd*`Zvigcpmip7t",
        "E8TplR(!:FnxTmZ9{eSni+^.%ln)",
        "(?YJ.M2^OAJ<!AXM%kp",
        "@DzziLxs^k|~fXC}z]#EIHi?5Xwzn",
        ",7&2V X~Ksa!U;",
        ".zxtNH+Z,#xon1slaz3bwU2\"XC*[<",
        "/BUWD.-%LiY1Wj7uq%0R^s",
        "Ii+VI[VMpUJ2UJPXC"
      )

      forAll(Gen.oneOf(invalidStrs)) { string =>

        val formData: MutableMap[String, String] = extraData += (fieldName -> string)
        val newForm = form.bind(Map(formData.toSeq.sortBy(_._1): _*))

        val result = newForm.apply(fieldName)
        result.errors shouldEqual Seq(regexError)
      }
    }
  }
}
