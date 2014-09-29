package fr.ortolang.diffusion.workflow.task;

import java.util.Date;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

public class HelloWorldTask implements JavaDelegate {

	@Override
	public void execute(DelegateExecution execution) {
		System.out.println("execution id " + execution.getId());
		String name = (String) execution.getVariable("name");
		System.out.println("Hello " + name);
		execution.setVariable("greettime", new Date());
	}

}
