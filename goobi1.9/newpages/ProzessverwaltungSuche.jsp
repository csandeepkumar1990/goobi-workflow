<%@ page session="false" contentType="text/html;charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://jsftutorials.net/htmLib" prefix="htm"%>
<%@ taglib uri="http://myfaces.apache.org/tomahawk" prefix="x"%>
<%@ taglib uri="http://sourceforge.net/projects/jsf-comp/easysi"
	prefix="si"%>
<%@ taglib uri="http://richfaces.org/rich" prefix="rich"%>
	
<%-- ######################################## 

								Suchmaske für Prozesse

	#########################################--%>

<html>
<f:view locale="#{SpracheForm.locale}">
	<%@include file="inc/head.jsp"%>
	<body>

		<htm:table cellspacing="5" cellpadding="0" styleClass="layoutTable"
			align="center">
			<%@include file="inc/tbl_Kopf.jsp"%>
			<htm:tr>
				<%@include file="inc/tbl_Navigation.jsp"%>
				<htm:td valign="top" styleClass="layoutInhalt">

					<%-- ++++++++++++++++     Inhalt      ++++++++++++++++ --%>
					<h:form id="procmanageform">
						<%-- Breadcrumb --%>
						<h:panelGrid width="100%" columns="1"
							styleClass="layoutInhaltKopf">
							<h:panelGroup>
								<h:commandLink value="#{msgs.startseite}" action="newMain"
									id="mainlink" />
								<f:verbatim> &#8250;&#8250; </f:verbatim>
								<h:outputText value="#{msgs.prozessverwaltung}" />
							</h:panelGroup>
						</h:panelGrid>

						<%-- Initialisierung von ProzessverwaltungForm --%>

						<h:panelGrid id="extended" columns="2"
							rendered="#{ProzessverwaltungForm.initialize}">

							<%-- process title --%>
							<h:outputText value="#{msgs.title}" />
							<h:inputText value="#{SearchForm.processTitle}" style="width:690px" />
							
							<%-- process id --%>
							<h:outputText value="#{msgs.id}" />
							<h:inputText value="#{SearchForm.idin}" style="width:690px"/>

							<%--projects --%>
							<h:outputText value="#{msgs.projects}" />
							<h:panelGroup>
								<h:selectOneMenu value="#{SearchForm.projectOperand}" style="width:70px">
									<f:selectItems value="#{SearchForm.operands}" />
								</h:selectOneMenu>

								<h:selectOneMenu value="#{SearchForm.project}" style="width:220px">
									<si:selectItems id="pcid11" value="#{SearchForm.projects}"
										var="proj" itemLabel="#{proj}" itemValue="#{proj}" />
								</h:selectOneMenu>
							</h:panelGroup>

							<%-- process property --%>
							<h:outputText value="#{msgs.processProperties}" />
							<h:panelGroup>
								<h:selectOneMenu value="#{SearchForm.processPropertyOperand}" style="width:70px">
									<f:selectItems value="#{SearchForm.operands}" />
								</h:selectOneMenu>
								<h:panelGroup>
									<h:selectOneMenu value="#{SearchForm.processPropertyTitle}" style="width:220px">
										<si:selectItems value="#{SearchForm.processPropertyTitles}"
											var="proc" itemLabel="#{proc}" itemValue="#{proc}" />
									</h:selectOneMenu>
									<h:inputText value="#{SearchForm.processPropertyValue}"  style="width:400px"/>
								</h:panelGroup>
							</h:panelGroup>

							<%-- masterpiece property --%>
							<h:outputText value="#{msgs.masterpieceProperties}" />
							<h:panelGroup>
								<h:selectOneMenu
									value="#{SearchForm.masterpiecePropertyOperand}" style="width:70px">
									<f:selectItems value="#{SearchForm.operands}" />
								</h:selectOneMenu>
								<h:panelGroup>
									<h:selectOneMenu value="#{SearchForm.masterpiecePropertyTitle}" style="width:220px">
										<si:selectItems
											value="#{SearchForm.masterpiecePropertyTitles}" var="work"
											itemLabel="#{work}" itemValue="#{work}" />
									</h:selectOneMenu>
									<h:inputText value="#{SearchForm.masterpiecePropertyValue}"  style="width:400px" />
								</h:panelGroup>
							</h:panelGroup>
							<%-- template property --%>
							<h:outputText value="#{msgs.templateProperties}" />
							<h:panelGroup>
								<h:selectOneMenu value="#{SearchForm.templatePropertyOperand}" style="width:70px">
									<f:selectItems value="#{SearchForm.operands}" />
								</h:selectOneMenu>
								<h:panelGroup>
									<h:selectOneMenu value="#{SearchForm.templatePropertyTitle}" style="width:220px">
										<si:selectItems value="#{SearchForm.templatePropertyTitles}"
											var="temp" itemLabel="#{temp}" itemValue="#{temp}" />
									</h:selectOneMenu>
									<h:inputText value="#{SearchForm.templatePropertyValue}"  style="width:400px" />
								</h:panelGroup>
							</h:panelGroup>
							<%-- step property --%>
							<h:outputText value="#{msgs.stepProperties}" />
							<h:panelGroup>
								<h:selectOneMenu value="#{SearchForm.stepPropertyOperand}" style="width:70px">
									<f:selectItems value="#{SearchForm.operands}" />
								</h:selectOneMenu>
								<h:panelGroup>
									<h:selectOneMenu value="#{SearchForm.stepPropertyTitle}" style="width:220px">
										<si:selectItems value="#{SearchForm.stepPropertyTitles}"
											var="step" itemLabel="#{step}" itemValue="#{step}" />
									</h:selectOneMenu>
									<h:inputText value="#{SearchForm.stepPropertyValue}"  style="width:400px" />
								</h:panelGroup>
							</h:panelGroup>
							<%--steps --%>
							<h:outputText value="#{msgs.step}" />
							<h:panelGroup>
								<h:selectOneMenu value="#{SearchForm.stepOperand}" style="width:70px">
									<f:selectItems value="#{SearchForm.operands}" />
								</h:selectOneMenu>
								<h:panelGroup>
									<h:selectOneMenu value="#{SearchForm.status}" style="width:220px">
										<si:selectItems value="#{SearchForm.stepstatus}"
											var="stepstatus" itemLabel="#{stepstatus.title}"
											itemValue="#{stepstatus.searchString}" />
									</h:selectOneMenu>
									<h:selectOneMenu value="#{SearchForm.stepname}" style="width:400px">
										<si:selectItems value="#{SearchForm.stepTitles}"
											var="stepTitles" itemLabel="#{stepTitles}"
											itemValue="#{stepTitles}" />
									</h:selectOneMenu>
								</h:panelGroup>
							</h:panelGroup>
							<%-- user --%>
							<%-- 
							<h:outputText value="#{msgs.user}"/>
							<h:panelGroup>
								<h:selectOneMenu value="#{SearchForm.stepdoneuser}">
										<si:selectItems value="#{SearchForm.user}"
										var="user" itemLabel="#{user.nachVorname}" itemValue="#{user.login}" />
								</h:selectOneMenu>
								<h:selectOneMenu value="#{SearchForm.stepdonetitle}">
									<si:selectItems value="#{SearchForm.stepTitles}"
										var="stepTitles" itemLabel="#{stepTitles}" itemValue="#{stepTitles}" />
								</h:selectOneMenu>
							</h:panelGroup>
							--%>
						</h:panelGrid>
						<h:commandButton action="#{SearchForm.filter}"
							title="#{msgs.filterAnwenden}" />


					</h:form>
					<%-- ++++++++++++++++    // Inhalt      ++++++++++++++++ --%>

				</htm:td>
			</htm:tr>
			<%@include file="inc/tbl_Fuss.jsp"%>
		</htm:table>

	</body>
</f:view>

</html>
