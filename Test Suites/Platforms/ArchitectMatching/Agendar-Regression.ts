<?xml version="1.0" encoding="UTF-8"?>
<TestSuiteEntity>
   <description>Suite de Architect Matching (TESTING). Activos: happy path del wizard /agendar (bloqueado al confirmar por bug conocido), regresion del server error ERROR 3466207270 (FALLA POR DISENO mientras el bug exista) y verificacion de mismatch de GTM Kit. Los placeholders de modulos del home quedan isRun=false hasta mapear detalle. Requiere VERCEL_BYPASS_TOKEN (o env VERCEL_AUTOMATION_BYPASS_SECRET). Cada TC abre su propio browser (app sin login).</description>
   <name>Agendar-Regression</name>
   <tag>ArchitectMatching,Regression,Agendar,Testing</tag>
   <isRerun>false</isRerun>
   <mailRecipient></mailRecipient>
   <numberOfRerun>0</numberOfRerun>
   <pageLoadTimeout>30</pageLoadTimeout>
   <pageLoadTimeoutDefault>true</pageLoadTimeoutDefault>
   <rerunFailedTestCasesOnly>false</rerunFailedTestCasesOnly>
   <rerunImmediately>false</rerunImmediately>
   <testCaseLink>
      <guid>8fe15bcd-ff1d-4897-a9f0-c7749978e393</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ArchitectMatching/agendar/happy-path-agendar-taller</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>4b813429-18c9-4092-a8cb-ca92501e2dd7</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ArchitectMatching/agendar/regression-confirmar-taller-server-error</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>8337ba75-2b67-4707-a5e2-8c99b2d7eec7</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ArchitectMatching/agendar/verificar-seleccion-gtm-kit</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>5baebf44-ea8c-4d55-a327-2da3978999f0</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ArchitectMatching/modulos/catalogo-kits-pendiente</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>bf8a3af6-5f2a-42a9-b6f3-84580040f855</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ArchitectMatching/modulos/ver-cuentas-pendiente</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>5e98de78-2470-49fd-b9bb-1419c6967b1b</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ArchitectMatching/modulos/gestionar-plataforma-pendiente</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
</TestSuiteEntity>
