/*
 * Copyright 2024 HM Revenue & Customs
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

package forms.mappings

import models.Country
import play.api.data.Forms.{optional, seq}
import play.api.data.Mapping

trait CountryListMappings extends AddressMappings {

  def countryListMapping[A](
    errorKey: String
  )(apply: Seq[Option[Country]] => A, unapply: A => Seq[Option[Country]]): Mapping[A] =
    seq(
      optional(
        text(errorKey)
          .verifying(countryConstraint())
          .transform[Country](parseCountry, _.code)
      )
    ).verifying(nonEmptyOptionalSeq(errorKey))
      .transform[A](apply, unapply)
}
