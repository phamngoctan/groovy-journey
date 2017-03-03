import static groovyx.net.http.Method.*;
import static groovyx.net.http.ContentType.*;
import groovy.json.*;

import com.axonivy.utils.CommonUtils;
import com.axonivy.scripting.RestClient;
import com.axonivy.converter.DataTableConverter;
import com.axonivy.model.DataTable;
import static com.axonivy.utils.Constants.*;
import groovyx.net.http.ParserRegistry;
import groovyx.net.http.RESTClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.io.File;
import java.io.FileInputStream;
import org.apache.http.entity.BufferedHttpEntity;

import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import groovyx.net.http.HTTPBuilder;
import static groovyx.net.http.Method.POST;
import org.apache.http.entity.ContentType;
import org.yaml.snakeyaml.Yaml

/*===========================================================================================
  =             This block is used for process data please do not touch on it               =
  ===========================================================================================
*/



/*
===============================================
=                   MAPPING                   =
===============================================
*/
ParserRegistry.setDefaultCharset("UTF-8");

final CONTRACT_TEMPORAL_UPDATE = [
'''\
companyName: "Process AG"
employeeName: "Massimo Rinaldi"
validFrom: 01.01.2014
validTo: 
tariffCode: "B1Y"
insuranceAllocations:
  - insuranceAllocation: "FAK003"
    employeeRate: ''
  - insuranceAllocation: "KTG1/10"
    employeeRate: ''
  - insuranceAllocation: "UVG1/A1"
    employeeRate: ''
  - insuranceAllocation: "UVGZ1/10"
    employeeRate: ''
employmentType: "MONTHLY_SALARY"
monthlySalary: 5000.00
engagementLevel: 100.00
'''
]

/*
===============================================
=               Utility classes               =
===============================================
*/

public class CommonUtils {

  public static final String DATE_FORMAT_USING_IN_GROOVY = "dd.MM.yyyy";
  public static final String DATE_TIME_SHORT_FORMAT_CONFORM_TO_ISO_8601 = "yyyy-MM-dd";

  public static LinkedHashMap convertFromReadableArrayListToJson(def sitData){
    Yaml yaml = new Yaml()
    def obj = yaml.load(sitData);
    return obj
  }

  public static String modifyDateFormat(String inputDate) {
    String valueAfterChangeFormat = null
    if (inputDate != null && inputDate != "") {
      LocalDate changedDate = LocalDate.parse(inputDate, DateTimeFormatter.ofPattern(DATE_FORMAT_USING_IN_GROOVY));
      valueAfterChangeFormat = changedDate.format(DateTimeFormatter.ofPattern(DATE_TIME_SHORT_FORMAT_CONFORM_TO_ISO_8601));
      valueAfterChangeFormat += "T00:00:00Z";
    }
    return valueAfterChangeFormat
  }

}

public class CivilInfoBuilder {
  Map civilInfo = new LinkedHashMap();
  CommonUtils commonUtils = new CommonUtils();

  private CivilInfoBuilder() {
  }

  public static CivilInfoBuilder getInstance() {
    return new CivilInfoBuilder();
  }

  public CivilInfoBuilder buildId(String id) {
    civilInfo.put('id', id);
    return this;
  }

  public CivilInfoBuilder buildValidDate(String ddmmYYYY) {
    civilInfo.put('validDate', commonUtils.modifyDateFormat(ddmmYYYY));
    return this;
  }

  public CivilInfoBuilder buildSpouseUri(String spouseUri) {
    civilInfo.put('spouseUri', spouseUri);
    return this;
  }

  public CivilInfoBuilder buildState(String state) {
    civilInfo.put('state', state);
    return this;
  }

  public Map get() {
    return civilInfo;
  }
}

public class ContractTemporalBuilder {

  Map contractTemporal = new LinkedHashMap();
  CommonUtils commonUtils = new CommonUtils();

  private ContractTemporalBuilder() {
  }

  public static ContractTemporalBuilder getInstance() {
    return new ContractTemporalBuilder();
  }

  public ContractTemporalBuilder buildId(String id) {
    contractTemporal.put('id', id);
    return this;
  }

  public ContractTemporalBuilder buildValidFrom(String validFrom) {
    contractTemporal.put('validFrom', commonUtils.modifyDateFormat(validFrom));
    return this;
  }

  public ContractTemporalBuilder buildValidTo(String validTo) {
    contractTemporal.put('validTo', commonUtils.modifyDateFormat(validTo));
    return this;
  }

  public ContractTemporalBuilder buildBvgDeduction(String bvgDeduction) {
    contractTemporal.put('bvgDeduction', bvgDeduction);
    return this;
  }

  public ContractTemporalBuilder buildEmploymentType(String employmentType) {
    contractTemporal.put('employmentType', employmentType);
    return this;
  }

  public ContractTemporalBuilder buildMonthlySalary(Double monthlySalary) {
    contractTemporal.put('monthlySalary', monthlySalary);
    return this;
  }

  public ContractTemporalBuilder buildEngagementLevel(Double engagementLevel) {
    contractTemporal.put('engagementLevel', engagementLevel);
    return this;
  }

  public ContractTemporalBuilder buildTariffCode(String tariffCode) {
    contractTemporal.put('tariffCode', tariffCode);
    return this;
  }

  public ContractTemporalBuilder buildInsuranceAllocations(List insuranceAllocationsRawData) {
    List insuranceAllocations = new ArrayList();
    
    insuranceAllocationsRawData.eachWithIndex{insuranceAllocationRawData, index ->
      println "Raw data index: " + index + " " + insuranceAllocationRawData;
      Map insuranceAllocation = new LinkedHashMap();
      insuranceAllocation.put('insuranceAllocation', insuranceAllocationRawData.getAt('insuranceAllocation'));
      insuranceAllocation.put('employeeRate', insuranceAllocationRawData.getAt('employeeRate'));
      insuranceAllocations.add(insuranceAllocation);
    };
    contractTemporal.put('insuranceAllocations', insuranceAllocations);
    return this;
  }

  public Map get() {
    return contractTemporal;
  }
}

/*
===============================================
=               BLOCK RUN CODE                =
===============================================
*/

try {
  returnMessage = 'Started script execution ' + new Date().toString() + '\r\n';

  tenantId = LUZ_COMPANY_TENANT;
  String token = LUZ_TOKEN;

  String compensationHost = binding.variables.containsKey("LUZ_COMPENSATION_HOST") ?
    LUZ_COMPENSATION_HOST : "http://localhost:8080/luz_compensation/";
  String personHost = binding.variables.containsKey("LUZ_PERSON_HOST") ?
    LUZ_PERSON_HOST : "http://localhost:8080/luz_person/";
  String tenantHost = binding.variables.containsKey("LUZ_TENANT_HOST") ?
    LUZ_PERSON_HOST : "http://localhost:8080/luztenant/";

  publicCompensationClient = new RESTClient(compensationHost + 'api/');
  publicCompensationClient.defaultRequestHeaders.'Authorization' = "Bearer " + token;
  publicCompensationClient.defaultRequestHeaders.'Accept' = 'application/json';
  tenantCompensationClient = new RESTClient(compensationHost + 'api/' + tenantId + '/');
  tenantCompensationClient.defaultRequestHeaders.'Authorization' = "Bearer " + token;
  tenantCompensationClient.defaultRequestHeaders.'Accept' = 'application/json';

  publicPersonClient = new RESTClient(personHost + 'api/');
  publicPersonClient.defaultRequestHeaders.'Authorization' = "Bearer " + token;
  publicPersonClient.defaultRequestHeaders.'Accept' = 'application/json';
  tenantPersonClient = new RESTClient(personHost + 'api/' + tenantId + '/');
  tenantPersonClient.defaultRequestHeaders.'Authorization' = "Bearer " + token;
  tenantPersonClient.defaultRequestHeaders.'Accept' = 'application/json';

  tenantClient = new RESTClient(tenantHost + 'api/' + tenantId + '/');
  tenantClient.defaultRequestHeaders.'Authorization' = "Bearer " + token;
  tenantClient.defaultRequestHeaders.'Accept' = 'application/json';

  //doLoopForEmployees();
  Map civilInfo = CivilInfoBuilder.getInstance().buildId('1').buildState('SINGLE').buildSpouseUri('/luz_person/api/72a46319-b066-4787-8710-04ce12bed738/persons/1').buildValidDate('06.06.1993').get();
  addReturnMessageLn("civilInfo ${civilInfo}");
  partialUpdateCivilInfoOfEmployee("Process AG", "Massimo Rinaldi", civilInfo, "replace");

  CONTRACT_TEMPORAL_UPDATE.each{contractTemporalItem -> 
    LinkedHashMap contractTemporalYamlData = CommonUtils.convertFromReadableArrayListToJson(contractTemporalItem);
    addReturnMessageLn("==> " + contractTemporalYamlData);

    addReturnMessageLn("${contractTemporalYamlData.getAt('insuranceAllocations')}");
    Map contractTemporal = ContractTemporalBuilder.getInstance()
      .buildValidFrom(contractTemporalYamlData.getAt('validFrom'))
      .buildValidTo(contractTemporalYamlData.getAt('validTo'))
      .buildTariffCode(contractTemporalYamlData.getAt('tariffCode'))
      .buildEngagementLevel(contractTemporalYamlData.getAt('engagementLevel'))
      .buildEmploymentType(contractTemporalYamlData.getAt('employmentType'))
      .buildBvgDeduction(contractTemporalYamlData.getAt('bvgDeduction'))
      .buildMonthlySalary(contractTemporalYamlData.getAt('monthlySalary'))
      .buildInsuranceAllocations(contractTemporalYamlData.getAt('insuranceAllocations'))
      .get();
    addReturnMessageLn("${contractTemporal}");
  };

  addReturnMessageLn("Finished testing " + new Date().toString());

} catch (AssertionError e) {
  addReturnMessageLn("\r\nFAILED:\r\n" + e.getMessage() + '\r\n@ ' + getScriptErrorLineInStackTrace(e));
} catch (groovyx.net.http.HttpResponseException e) {
  def text = (e.getResponse().getData() instanceof java.io.Reader
    || e.getResponse().getData() instanceof java.io.ByteArrayInputStream)
    ? e.getResponse().getData().text
    : e.getResponse().getData().toString();
  addReturnMessageLn(text + '\r\n@ ' + getScriptErrorLineInStackTrace(e));
}
return returnMessage;

/*
===============================================
=               Common methods                =
===============================================
*/

void addReturnMessageLn(String msg) {
  returnMessage += msg + '\r\n';
}

String getScriptErrorLineInStackTrace(Throwable e) {
  return e.getStackTrace().findAll{line -> line.toString().contains('Script1')};
}

/*
===============================================
=               IMPLEMENT CODE                =
===============================================
*/

private void partialUpdateCivilInfoOfEmployee(String companyName, String employeeName, Map civilInfo, String option) {
  companyByNameResponse = tenantCompensationClient.get(
    path: "companies",
    query:[name: companyName]);
  assert companyByNameResponse.status == 200 : "Search company FAILED!"
  assert companyByNameResponse.data.size() != 0 : "There is no employee with name: ${companyName}";
  addReturnMessageLn("Company size: " + companyByNameResponse.data.size());
  addReturnMessageLn("--> companyId: " + companyByNameResponse.data[0].id);
  companyId = companyByNameResponse.data[0].id;

  employeeByNameResponse = tenantCompensationClient.get(
    path: "companies/${companyId}/employees",
    query:[name: employeeName]);
  assert employeeByNameResponse.status == 200 : "Search employee FAILED!";
  assert employeeByNameResponse.data.size() != 0 : "There is no employee with name: ${employeeName}";
  addReturnMessageLn("Employee size: " + employeeByNameResponse.data.size());
  addReturnMessageLn("--> companyId: " + employeeByNameResponse.data[0].id);
  employeeId = companyByNameResponse.data[0].id;

  List partialUpdateList = new ArrayList();
  Map partialObject = new LinkedHashMap();
  partialObject.put("op", option);
  partialObject.put("path", "/civilInfo");
  
  partialObject.put('value', civilInfo);
  partialUpdateList.add(partialObject);

  addReturnMessageLn("--> partialUpdateList: ${partialUpdateList}");
  response = tenantCompensationClient.patch(
    path: "companies/${companyId}/employees/${employeeId}",
    body: JsonOutput.toJson(partialUpdateList),
    requestContentType: JSON);
  println "response.status: "  + response.status
  assert response.status == 200 : "Couldn't do the patch employee ${employeeId}!"
}


