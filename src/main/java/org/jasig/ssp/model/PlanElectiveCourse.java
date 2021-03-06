/**
 * Licensed to Apereo under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Apereo licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jasig.ssp.model;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import javax.persistence.OrderBy;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Table(name="map_plan_elective_course")
public class PlanElectiveCourse extends AbstractMapElectiveCourse implements Cloneable {

	/**
	 *
	 */
	private static final long serialVersionUID = -5473230782807660690L;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "plan_id", updatable = false, nullable = false)
	private Plan plan;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "planElectiveCourse", orphanRemoval=true)
	@Cascade({ CascadeType.PERSIST, CascadeType.MERGE, CascadeType.SAVE_UPDATE })
	@OrderBy("formattedCourse")
	private List<PlanElectiveCourseElective> planElectiveCourseElectives = new ArrayList<PlanElectiveCourseElective>(0);

	public List<PlanElectiveCourseElective> getElectiveCourseElectives() {
		return planElectiveCourseElectives;
	}

	public void setElectiveCourseElectives(List<PlanElectiveCourseElective> planElectiveCourseElectives) {
		this.planElectiveCourseElectives = planElectiveCourseElectives;
	}

	public Plan getPlan() {
		return plan;
	}

	public void setPlan(Plan plan) {
		this.plan = plan;
	}

	public Plan getParent() {
		return plan;
	}

	@Override
	protected PlanElectiveCourse clone() throws CloneNotSupportedException {
		PlanElectiveCourse clone = new PlanElectiveCourse();
		cloneCommonFields(clone);
		for (PlanElectiveCourseElective planElectiveCourseElective : this.getElectiveCourseElectives()) {
			PlanElectiveCourseElective planElectiveCourseElectiveClone = planElectiveCourseElective.clone();
			planElectiveCourseElectiveClone.setPlanElectiveCourse(clone);
			clone.getElectiveCourseElectives().add(planElectiveCourseElectiveClone);
		}
		return clone;
	}

}
