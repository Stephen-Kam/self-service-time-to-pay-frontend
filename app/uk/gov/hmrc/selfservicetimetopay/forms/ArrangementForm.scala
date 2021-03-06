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

package uk.gov.hmrc.selfservicetimetopay.forms

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.selfservicetimetopay.models.ArrangementDayOfMonth

import scala.util.control.Exception._

object ArrangementForm {

  val dayOfMonthForm = {
    def tryToInt(input: String) = {
      catching(classOf[NumberFormatException]) opt input.toInt
    }
    def isInt(input: String) = {
      tryToInt(input).nonEmpty
    }
    Form(mapping(
      "dayOfMonth" -> text
        .verifying("ssttp.arrangement.instalment-summary.payment-day.required", { i: String => (i != null) && i.nonEmpty })
        .verifying("ssttp.arrangement.instalment-summary.payment-day.out-of-range", { i => i.isEmpty || (i.nonEmpty && isInt(i)) })
        .verifying("ssttp.arrangement.instalment-summary.payment-day.out-of-range", { i => !isInt(i) || (isInt(i) && (i.toInt >= 1)) })
        .verifying("ssttp.arrangement.instalment-summary.payment-day.out-of-range", { i => !isInt(i) || (isInt(i) && (i.toInt <= 28)) })
    )(dayOfMonth => ArrangementDayOfMonth(tryToInt(dayOfMonth).get))(data => Some(data.dayOfMonth.toString))
    )
  }
}
