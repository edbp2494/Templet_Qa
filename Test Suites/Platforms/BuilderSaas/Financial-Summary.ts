<?xml version="1.0" encoding="UTF-8"?>
<TestSuiteEntity>
   <description>[E9 · US-03] Financial Summary Dashboard del Builder SaaS en una sola sesion de navegador (login mock por ?role=, sin SSO): acceso ContractOwner via sidebar, contenido + consistencia de data (element-map), guard permitido (Admin) y guard denegado (Specialist — falla por diseño hasta que implementen RBAC). Suite separada de Smoke.ts porque no usa login Microsoft.</description>
   <name>Financial-Summary</name>
   <tag>NoRestart,BuilderSaas,FinancialSummary,RBAC,E9-US03</tag>
   <isRerun>false</isRerun>
   <mailRecipient></mailRecipient>
   <numberOfRerun>0</numberOfRerun>
   <pageLoadTimeout>30</pageLoadTimeout>
   <pageLoadTimeoutDefault>true</pageLoadTimeoutDefault>
   <rerunFailedTestCasesOnly>false</rerunFailedTestCasesOnly>
   <rerunImmediately>false</rerunImmediately>
   <testSuiteGuid>6d3f9b4e-8a2c-4d7b-9e6f-1c5a8d2b7f39</testSuiteGuid>
   <testCaseLink>
      <guid>8f2a6d4b-7c3e-4b9a-a1d8-3e6c9f2b5d17</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/BuilderSaas/financial-summary/validate-access-contractowner</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>3c7e9a2f-4d6b-4e8c-b9a3-6f1d8c4e2a75</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/BuilderSaas/financial-summary/validate-dashboard-content</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>5b9d3f7a-2e8c-4a6d-8f2b-9c4e7a1d3b58</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/BuilderSaas/financial-summary/validate-guard-admin</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>e2c6a8d4-5f3b-4d9e-a7c1-4b8f6d2e9a35</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/BuilderSaas/financial-summary/validate-guard-specialist-denied</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
</TestSuiteEntity>
