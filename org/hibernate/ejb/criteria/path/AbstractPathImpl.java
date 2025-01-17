/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.ejb.criteria.path;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;

import org.hibernate.ejb.criteria.CriteriaBuilderImpl;
import org.hibernate.ejb.criteria.CriteriaQueryCompiler;
import org.hibernate.ejb.criteria.ParameterRegistry;
import org.hibernate.ejb.criteria.PathImplementor;
import org.hibernate.ejb.criteria.PathSource;
import org.hibernate.ejb.criteria.expression.ExpressionImpl;
import org.hibernate.ejb.criteria.expression.PathTypeExpression;

/**
 * Convenience base class for various {@link Path} implementors.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractPathImpl<X>
		extends ExpressionImpl<X>
		implements Path<X>, PathImplementor<X>, Serializable {

	private final PathSource pathSource;
	private final Expression<Class<? extends X>> typeExpression;
	private Map<String,Path> attributePathRegistry;

	/**
	 * Constructs a basic path instance.
	 *
	 * @param criteriaBuilder The criteria builder
	 * @param javaType The java type of this path
	 * @param pathSource The source (or origin) from which this path originates
	 */
	@SuppressWarnings({ "unchecked" })
	public AbstractPathImpl(
			CriteriaBuilderImpl criteriaBuilder,
			Class<X> javaType,
			PathSource pathSource) {
		super( criteriaBuilder, javaType );
		this.pathSource = pathSource;
		this.typeExpression =  new PathTypeExpression( criteriaBuilder(), getJavaType(), this );
	}

	public PathSource getPathSource() {
		return pathSource;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public PathSource<?> getParentPath() {
        return getPathSource();
    }

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked" })
	public Expression<Class<? extends X>> type() {
		return typeExpression;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getPathIdentifier() {
		return getPathSource().getPathIdentifier() + "." + getAttribute().getName();
	}

	protected abstract boolean canBeDereferenced();

	protected final RuntimeException illegalDereference() {
		String message = "Illegal attempt to dereference path source";
		PathSource<?> source = getPathSource();
		if ( source != null ) {
			message += " [" + getPathSource().getPathIdentifier() + "]";
		}
		return new IllegalArgumentException(message);
	}

	protected final RuntimeException unknownAttribute(String attributeName) {
		String message = "Unable to resolve attribute [" + attributeName + "] against path";
		PathSource<?> source = getPathSource();
		if ( source != null ) {
			message += " [" + source.getPathIdentifier() + "]";
		}
		return new IllegalArgumentException(message);
	}

	protected final Path resolveCachedAttributePath(String attributeName) {
		return attributePathRegistry == null
				? null
				: attributePathRegistry.get( attributeName );
	}

	protected final void registerAttributePath(String attributeName, Path path) {
		if ( attributePathRegistry == null ) {
			attributePathRegistry = new HashMap<String,Path>();
		}
		attributePathRegistry.put( attributeName, path );
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked" })
	@Override
	public <Y> Path<Y> get(SingularAttribute<? super X, Y> attribute) {
		if ( ! canBeDereferenced() ) {
			throw illegalDereference();
		}

		SingularAttributePath<Y> path = (SingularAttributePath<Y>) resolveCachedAttributePath( attribute.getName() );
		if ( path == null ) {
			path = new SingularAttributePath<Y>( criteriaBuilder(), attribute.getJavaType(), this, attribute );
			registerAttributePath( attribute.getName(), path );
		}
		return path;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked" })
	@Override
	public <E, C extends Collection<E>> Expression<C> get(PluralAttribute<X, C, E> attribute) {
		if ( ! canBeDereferenced() ) {
			throw illegalDereference();
		}

		PluralAttributePath<C> path = (PluralAttributePath<C>) resolveCachedAttributePath( attribute.getName() );
		if ( path == null ) {
			path = new PluralAttributePath<C>( criteriaBuilder(), this, attribute );
			registerAttributePath( attribute.getName(), path );
		}
		return path;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked" })
	@Override
	public <K, V, M extends Map<K, V>> Expression<M> get(MapAttribute<X, K, V> attribute) {
		if ( ! canBeDereferenced() ) {
			throw illegalDereference();
		}

		PluralAttributePath path = (PluralAttributePath) resolveCachedAttributePath( attribute.getName() );
		if ( path == null ) {
			path = new PluralAttributePath( criteriaBuilder(), this, attribute );
			registerAttributePath( attribute.getName(), path );
		}
		return path;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked" })
	@Override
	public <Y> Path<Y> get(String attributeName) {
		if ( ! canBeDereferenced() ) {
			throw illegalDereference();
		}

		final Attribute attribute = locateAttribute( attributeName );

		if ( attribute.isCollection() ) {
			final PluralAttribute<X,Y,?> pluralAttribute = (PluralAttribute<X,Y,?>) attribute;
			if ( PluralAttribute.CollectionType.MAP.equals( pluralAttribute.getCollectionType() ) ) {
				return (PluralAttributePath<Y>) this.<Object,Object,Map<Object, Object>>get( (MapAttribute) pluralAttribute );
			}
			else {
				return (PluralAttributePath<Y>) this.get( (PluralAttribute) pluralAttribute );
			}
		}
		else {
			return get( (SingularAttribute<X,Y>) attribute );
		}
	}

	/**
	 * Get the attribute by name from the underlying model.  This allows subclasses to
	 * define exactly how the attribute is derived.
	 *
	 * @param attributeName The name of the attribute to locate
	 *
	 * @return The attribute; should never return null.
	 *
	 * @throws IllegalArgumentException If no such attribute exists
	 */
	protected  final Attribute locateAttribute(String attributeName) {
		final Attribute attribute = locateAttributeInternal( attributeName );
		if ( attribute == null ) {
			throw unknownAttribute( attributeName );
		}
		return attribute;
	}

	/**
	 * Get the attribute by name from the underlying model.  This allows subclasses to
	 * define exactly how the attribute is derived.  Called from {@link #locateAttribute}
	 * which also applies nullness checking for proper error reporting.
	 *
	 * @param attributeName The name of the attribute to locate
	 *
	 * @return The attribute; may be null.
	 *
	 * @throws IllegalArgumentException If no such attribute exists
	 */
	protected abstract Attribute locateAttributeInternal(String attributeName);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void registerParameters(ParameterRegistry registry) {
		// none to register
	}
	@Override
	public void prepareAlias(CriteriaQueryCompiler.RenderingContext renderingContext) {
		// Make sure we delegate up to our source (eventually up to the path root) to
		// prepare the path properly.
		PathSource<?> source = getPathSource();
		if ( source != null ) {
			source.prepareAlias( renderingContext );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String render(CriteriaQueryCompiler.RenderingContext renderingContext) {
		PathSource<?> source = getPathSource();
		if ( source != null ) {
			source.prepareAlias( renderingContext );
			return source.getPathIdentifier() + "." + getAttribute().getName();
		} else {
			return getAttribute().getName();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String renderProjection(CriteriaQueryCompiler.RenderingContext renderingContext) {
		return render( renderingContext );
	}
}
