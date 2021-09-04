
package acme.features.anonymous.workPlan;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import acme.entities.workPlans.WorkPlan;
import acme.features.anonymous.task.AnonymousTaskRepository;
import acme.framework.components.Model;
import acme.framework.components.Request;
import acme.framework.entities.Anonymous;
import acme.framework.services.AbstractShowService;

@Service
public class AnonymousWorkPlanShowService implements AbstractShowService<Anonymous, WorkPlan> {

	// Internal state ---------------------------------------------------------

	@Autowired
	protected AnonymousWorkPlanRepository repository;
	@Autowired
	protected AnonymousTaskRepository taskRepository;

	// AbstractListService<Administrator, Task> interface --------------


	@Override
	public boolean authorise(final Request<WorkPlan> request) {
		assert request != null;

		return true;
	}

	@Override
	public void unbind(final Request<WorkPlan> request, final WorkPlan entity, final Model model) {
		assert request != null;
		assert entity != null;
		assert model != null;

		request.unbind(entity, model, "title", "tasks", "executionStart", "executionEnd", "isPrivate", "tasksParsed", "workloadParsed");

	}

	@Override
	public WorkPlan findOne(final Request<WorkPlan> request) {
		assert request != null;
		WorkPlan result;
		int id;
		id = request.getModel().getInteger("id");
		result = this.repository.findOnePublicAndNotFinished(id);

		return result;
	}

}