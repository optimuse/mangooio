<#include "header.ftl">
<div class="row">
	<div class="col-xs-12">
		<div class="box">
	    	<div class="box-header">
	    		<h3>Metrics</h3>
	    	</div>
	    </div>
	</div>
</div>
<div class="row">
	<div class="col-xs-12">
    	<div class="box">
	    	<div class="box-header">
				<div class="form-group">
	            	<input type="text" name="table_search" id="filter" class="form-control" placeholder="Start typing what you are looking for...">
	            </div>
	        </div>
            <div class="box-body table-responsive no-padding">
            	<table class="table table-hover">
                	<thead>
						<tr>
							<th data-sort="string"><b>Key</b></th>
							<th data-sort="string"><b>Value</b></th>
						</tr>
					</thead>
					<tbody class="searchable">
                    	<#list metrics?keys as prop>
							<tr>
								<td>${prop}</td>
								<td>${metrics?api.get(prop)}</td>
							</tr>
						</#list>
                	</tbody>
                </table>
        	</div>
    	</div>
	</div>
</div>
<#include "footer.ftl">