/*
 * Copyright 2017 HM Revenue & Customs
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

package models.businessmatching

import org.scalatestplus.play.PlaySpec
import jto.validation.{Valid, Path, Invalid}
import jto.validation.ValidationError

class TypeOfBusinessSpec extends PlaySpec {

  "TypeOfBusiness" must {

    "validate form Read" in {
      val formInput = Map("typeOfBusiness" -> Seq("sometext"))
      TypeOfBusiness.formRead.validate(formInput) must be(Valid(TypeOfBusiness("sometext")))
    }

    "throw error when required field is missing" in {
      val formInput = Map("typeOfBusiness" -> Seq(""))
      TypeOfBusiness.formRead.validate(formInput) must be(Invalid(Seq((Path \ "typeOfBusiness", Seq(ValidationError("error.required.bm.businesstype.type"))))))
    }

    "throw error when input exceeds max length" in {
      val formInput = Map("typeOfBusiness" -> Seq("sometext"*10))
      TypeOfBusiness.formRead.validate(formInput) must be(Invalid(Seq((Path \ "typeOfBusiness") -> Seq(ValidationError("error.max.length.bm.businesstype.type")))))
    }

    "throw error given invalid characters" in {
      val formInput = Map("typeOfBusiness" -> Seq("abc{}abc"))
      TypeOfBusiness.formRead.validate(formInput) must be (Invalid(Seq((Path \ "typeOfBusiness", Seq(ValidationError("err.text.validation"))))))
    }

    "throw error given whitespace only" in {
      val formInput = Map("typeOfBusiness" -> Seq("     "))
      TypeOfBusiness.formRead.validate(formInput) must be (Invalid(Seq((Path \ "typeOfBusiness", Seq(ValidationError("error.required.bm.businesstype.type"))))))
    }

    "validate form write" in {
      TypeOfBusiness.formWrite.writes(TypeOfBusiness("sometext")) must be(Map("typeOfBusiness" -> Seq("sometext")))
    }



  }
}



