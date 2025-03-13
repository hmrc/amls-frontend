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

package models.businessdetails

import models.Country

case class CorrespondenceAddressNonUk(
  yourName: String,
  businessName: String,
  addressLineNonUK1: String,
  addressLineNonUK2: Option[String],
  addressLineNonUK3: Option[String],
  addressLineNonUK4: Option[String],
  country: Country
) {

  def toLines: Seq[String] =
    Seq(
      Some(yourName),
      Some(businessName),
      Some(addressLineNonUK1),
      addressLineNonUK2,
      addressLineNonUK3,
      addressLineNonUK4,
      Some(country.toString)
    ).flatten
}

object CorrespondenceAddressNonUk
