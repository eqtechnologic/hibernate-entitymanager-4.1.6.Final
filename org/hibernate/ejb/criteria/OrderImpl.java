/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ejb.criteria;
import java.io.Serializable;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;

/**
 * Represents an <tt>ORDER BY</tt> fragment.
 *
 * @author Steve Ebersole
 */
public class OrderImpl implements Order, Serializable {
	private final Expression<?> expression;
	private boolean ascending;

	public OrderImpl(Expression<?> expression) {
		this( expression, true );
	}

	public OrderImpl(Expression<?> expression, boolean ascending) {
		this.expression = expression;
		this.ascending = ascending;
	}

	public Order reverse() {
		ascending = !ascending;
		return this;
	}

	public boolean isAscending() {
		return ascending;
	}

	public Expression<?> getExpression() {
		return expression;
	}
}
