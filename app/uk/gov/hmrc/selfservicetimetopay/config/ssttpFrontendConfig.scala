/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.selfservicetimetopay.config

import play.api.Play.{configuration, current}
import uk.gov.hmrc.play.config.ServicesConfig

trait AppConfig {
  val assetsPrefix: String
  val analyticsToken: String
  val analyticsHost: String
  val reportAProblemPartialUrl: String
  val reportAProblemNonJSUrl: String
}

object SsttpFrontendConfig extends AppConfig with ServicesConfig {

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  private val contactHost = configuration.getString("contact-frontend.host").getOrElse("")
  private val contactFormServiceIdentifier = "self-service-time-to-pay"

  override lazy val assetsPrefix = loadConfig("assets.url") + loadConfig("assets.version")
  override lazy val analyticsToken = loadConfig("google-analytics.token")
  override lazy val analyticsHost = loadConfig("google-analytics.host")
  override lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  override lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"

  private lazy val companyAuthFrontend = baseUrl("company-auth")
  private lazy val companyAuthSignInPath = getConfString("company-auth.sign-in-path", "")
  lazy val loginUrl: String = s"$companyAuthFrontend$companyAuthSignInPath"
  lazy val loginCallbackBaseUrl = getConfString("auth.login-callback.base-url", "")
}
