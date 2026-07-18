<?xml version="1.0" encoding="UTF-8"?>
<TestSuiteEntity>
   <description>Suite de Visit Planner (TESTING). Activo: smoke del selector de perfil (unica pantalla mapeada). Los esqueletos de Comercial (prioridad: reclamar ventana + crear brief) y C-Level quedan isRun=false con TODOs de selectores — activarlos a medida que se mapee el DOM. Requiere VERCEL_BYPASS_TOKEN (o env VERCEL_AUTOMATION_BYPASS_SECRET).</description>
   <name>Smoke</name>
   <tag>VisitPlanner,Smoke,Testing</tag>
   <isRerun>false</isRerun>
   <mailRecipient></mailRecipient>
   <numberOfRerun>0</numberOfRerun>
   <pageLoadTimeout>30</pageLoadTimeout>
   <pageLoadTimeoutDefault>true</pageLoadTimeoutDefault>
   <rerunFailedTestCasesOnly>false</rerunFailedTestCasesOnly>
   <rerunImmediately>false</rerunImmediately>
   <testCaseLink>
      <guid>c1994e3d-896c-4257-b1f0-926efa0e2f74</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/VisitPlanner/smoke/seleccion-perfil</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>b5df93be-1616-48b1-b4ae-0f4388501d58</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>false</isRun>
      <testCaseId>Test Cases/VisitPlanner/comercial/reclamar-ventana-crear-brief</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>00051eee-52f0-4254-aad3-23032d51c82a</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>false</isRun>
      <testCaseId>Test Cases/VisitPlanner/comercial/ventana-menor-15-dias-no-reclamable</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>0babd2d7-c88f-4d06-92e3-847d2e4bc1e5</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>false</isRun>
      <testCaseId>Test Cases/VisitPlanner/comercial/alerta-feriado-en-ventana</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>64d66f8a-bc8c-466b-8bca-4a06c2a7671f</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>false</isRun>
      <testCaseId>Test Cases/VisitPlanner/clevel/ingresar-ventana-disponibilidad</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>f3513303-00a9-4b89-8af3-04fed35d703a</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>false</isRun>
      <testCaseId>Test Cases/VisitPlanner/clevel/no-desmarcar-ventana-con-reuniones</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
</TestSuiteEntity>
